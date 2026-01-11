import { useState } from "react";
import { trpc } from "@/lib/trpc";
import GameLayout from "@/components/GameLayout";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle, DialogTrigger } from "@/components/ui/dialog";
import { Textarea } from "@/components/ui/textarea";
import { Loader2, FileText, Plus, Building2, Package, DollarSign, Clock, CheckCircle2, XCircle, AlertTriangle, Send, ArrowRight, Calendar, Percent } from "lucide-react";
import { useToast } from "@/hooks/use-toast";

export default function Contracts() {
  const { toast } = useToast();
  const [dialogOpen, setDialogOpen] = useState(false);
  const [itemDialogOpen, setItemDialogOpen] = useState(false);
  const [selectedContract, setSelectedContract] = useState<number | null>(null);
  const [statusFilter, setStatusFilter] = useState<string>("all");

  // Form state for new contract
  const [buyerCompanyId, setBuyerCompanyId] = useState<string>("");
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [startTurn, setStartTurn] = useState(1);
  const [endTurn, setEndTurn] = useState(100);
  const [deliveryFrequency, setDeliveryFrequency] = useState<string>("per_turn");
  const [latePenalty, setLatePenalty] = useState(5);
  const [qualityPenalty, setQualityPenalty] = useState(10);

  // Form state for new contract item
  const [resourceTypeId, setResourceTypeId] = useState<string>("");
  const [quantityPerDelivery, setQuantityPerDelivery] = useState(100);
  const [totalQuantity, setTotalQuantity] = useState(1000);
  const [pricePerUnit, setPricePerUnit] = useState(10);
  const [minQuality, setMinQuality] = useState(0.8);

  const { data: companies } = trpc.company.all.useQuery();
  const { data: myCompany } = trpc.company.mine.useQuery();
  const { data: resources } = trpc.resource.list.useQuery();
  const { data: gameState } = trpc.game.state.useQuery();

  const { data: contracts, refetch: refetchContracts, isLoading } = trpc.contracts.list.useQuery(
    statusFilter === "all" ? {} : { status: statusFilter as "draft" | "proposed" | "negotiating" | "active" | "completed" | "cancelled" | "breached" }
  );

  const { data: contractDetails, refetch: refetchDetails } = trpc.contracts.byId.useQuery(
    { id: selectedContract! },
    { enabled: !!selectedContract }
  );

  const createContract = trpc.contracts.create.useMutation({
    onSuccess: (contract) => {
      toast({
        title: "Contract Created",
        description: "Contract draft has been created.",
      });
      setSelectedContract(contract.id);
      setDialogOpen(false);
      refetchContracts();
      // Reset form
      setBuyerCompanyId("");
      setTitle("");
      setDescription("");
    },
    onError: (error) => {
      toast({
        title: "Error",
        description: error.message,
        variant: "destructive",
      });
    },
  });

  const addItem = trpc.contracts.addItem.useMutation({
    onSuccess: () => {
      toast({
        title: "Item Added",
        description: "Contract item has been added.",
      });
      setItemDialogOpen(false);
      refetchDetails();
      refetchContracts();
      // Reset form
      setResourceTypeId("");
      setQuantityPerDelivery(100);
      setTotalQuantity(1000);
      setPricePerUnit(10);
    },
    onError: (error) => {
      toast({
        title: "Error",
        description: error.message,
        variant: "destructive",
      });
    },
  });

  const proposeContract = trpc.contracts.propose.useMutation({
    onSuccess: () => {
      toast({
        title: "Contract Proposed",
        description: "Contract has been sent to the other party.",
      });
      refetchContracts();
      refetchDetails();
    },
    onError: (error) => {
      toast({
        title: "Error",
        description: error.message,
        variant: "destructive",
      });
    },
  });

  const acceptContract = trpc.contracts.accept.useMutation({
    onSuccess: () => {
      toast({
        title: "Contract Accepted",
        description: "Contract is now active!",
      });
      refetchContracts();
      refetchDetails();
    },
    onError: (error) => {
      toast({
        title: "Error",
        description: error.message,
        variant: "destructive",
      });
    },
  });

  const rejectContract = trpc.contracts.reject.useMutation({
    onSuccess: () => {
      toast({
        title: "Contract Rejected",
        description: "Contract has been cancelled.",
      });
      refetchContracts();
      refetchDetails();
    },
    onError: (error) => {
      toast({
        title: "Error",
        description: error.message,
        variant: "destructive",
      });
    },
  });

  const getCompanyName = (id: number) => {
    return companies?.find((c) => c.id === id)?.name || "Unknown";
  };

  const getResourceName = (id: number) => {
    return resources?.find((r) => r.id === id)?.name || "Unknown";
  };

  const getStatusBadge = (status: string) => {
    switch (status) {
      case "draft":
        return <Badge variant="secondary">Draft</Badge>;
      case "proposed":
        return <Badge className="bg-blue-500/20 text-blue-500 border-blue-500/50">Proposed</Badge>;
      case "negotiating":
        return <Badge className="bg-yellow-500/20 text-yellow-500 border-yellow-500/50">Negotiating</Badge>;
      case "active":
        return <Badge className="bg-green-500/20 text-green-500 border-green-500/50"><CheckCircle2 className="w-3 h-3 mr-1" /> Active</Badge>;
      case "completed":
        return <Badge variant="outline" className="text-green-500"><CheckCircle2 className="w-3 h-3 mr-1" /> Completed</Badge>;
      case "cancelled":
        return <Badge variant="outline" className="text-muted-foreground"><XCircle className="w-3 h-3 mr-1" /> Cancelled</Badge>;
      case "breached":
        return <Badge variant="destructive"><AlertTriangle className="w-3 h-3 mr-1" /> Breached</Badge>;
      default:
        return <Badge variant="outline">{status}</Badge>;
    }
  };

  const getDeliveryFrequencyLabel = (freq: string) => {
    switch (freq) {
      case "per_turn": return "Every Turn";
      case "weekly": return "Weekly";
      case "monthly": return "Monthly";
      case "quarterly": return "Quarterly";
      case "one_time": return "One-time";
      default: return freq;
    }
  };

  const otherCompanies = companies?.filter((c) => c.id !== myCompany?.id);

  const handleCreateContract = () => {
    if (!buyerCompanyId || !title) return;
    createContract.mutate({
      buyerCompanyId: parseInt(buyerCompanyId),
      title,
      description,
      startTurn: startTurn || gameState?.currentTurn || 1,
      endTurn,
      deliveryFrequency: deliveryFrequency as "per_turn" | "weekly" | "monthly" | "quarterly" | "one_time",
      latePenaltyPercent: latePenalty,
      qualityPenaltyPercent: qualityPenalty,
    });
  };

  const handleAddItem = () => {
    if (!selectedContract || !resourceTypeId) return;
    addItem.mutate({
      contractId: selectedContract,
      resourceTypeId: parseInt(resourceTypeId),
      quantityPerDelivery,
      totalQuantity,
      pricePerUnit,
      minQuality,
    });
  };

  // Calculate contract stats
  const activeContracts = contracts?.filter((c) => c.status === "active").length || 0;
  const pendingContracts = contracts?.filter((c) => c.status === "proposed" || c.status === "draft").length || 0;
  const totalValue = contracts?.reduce((sum, c) => sum + parseFloat(c.totalValue), 0) || 0;

  if (isLoading) {
    return (
      <GameLayout>
        <div className="flex items-center justify-center h-64">
          <Loader2 className="w-8 h-8 animate-spin text-primary" />
        </div>
      </GameLayout>
    );
  }

  return (
    <GameLayout>
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-3xl font-bold">Contracts</h1>
            <p className="text-muted-foreground">Manage supply agreements with other companies</p>
          </div>
          <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
            <DialogTrigger asChild>
              <Button>
                <Plus className="w-4 h-4 mr-2" />
                New Contract
              </Button>
            </DialogTrigger>
            <DialogContent className="max-w-2xl">
              <DialogHeader>
                <DialogTitle>Create Supply Contract</DialogTitle>
                <DialogDescription>
                  Draft a new supply agreement with another company
                </DialogDescription>
              </DialogHeader>
              <div className="grid gap-4 py-4">
                <div className="grid grid-cols-2 gap-4">
                  <div className="space-y-2">
                    <Label>Buyer Company</Label>
                    <Select value={buyerCompanyId} onValueChange={setBuyerCompanyId}>
                      <SelectTrigger>
                        <SelectValue placeholder="Select company" />
                      </SelectTrigger>
                      <SelectContent>
                        {otherCompanies?.map((company) => (
                          <SelectItem key={company.id} value={company.id.toString()}>
                            {company.name}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </div>
                  <div className="space-y-2">
                    <Label>Delivery Frequency</Label>
                    <Select value={deliveryFrequency} onValueChange={setDeliveryFrequency}>
                      <SelectTrigger>
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="per_turn">Every Turn</SelectItem>
                        <SelectItem value="weekly">Weekly</SelectItem>
                        <SelectItem value="monthly">Monthly</SelectItem>
                        <SelectItem value="quarterly">Quarterly</SelectItem>
                        <SelectItem value="one_time">One-time</SelectItem>
                      </SelectContent>
                    </Select>
                  </div>
                </div>

                <div className="space-y-2">
                  <Label>Contract Title</Label>
                  <Input
                    value={title}
                    onChange={(e) => setTitle(e.target.value)}
                    placeholder="e.g., Iron Ore Supply Agreement"
                  />
                </div>

                <div className="space-y-2">
                  <Label>Description (Optional)</Label>
                  <Textarea
                    value={description}
                    onChange={(e) => setDescription(e.target.value)}
                    placeholder="Contract details and terms..."
                  />
                </div>

                <div className="grid grid-cols-2 gap-4">
                  <div className="space-y-2">
                    <Label>Start Turn</Label>
                    <Input
                      type="number"
                      min={gameState?.currentTurn || 1}
                      value={startTurn}
                      onChange={(e) => setStartTurn(parseInt(e.target.value) || 1)}
                    />
                  </div>
                  <div className="space-y-2">
                    <Label>End Turn</Label>
                    <Input
                      type="number"
                      min={startTurn + 1}
                      value={endTurn}
                      onChange={(e) => setEndTurn(parseInt(e.target.value) || 100)}
                    />
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-4">
                  <div className="space-y-2">
                    <Label>Late Delivery Penalty (%)</Label>
                    <Input
                      type="number"
                      min={0}
                      max={100}
                      value={latePenalty}
                      onChange={(e) => setLatePenalty(parseInt(e.target.value) || 0)}
                    />
                  </div>
                  <div className="space-y-2">
                    <Label>Quality Penalty (%)</Label>
                    <Input
                      type="number"
                      min={0}
                      max={100}
                      value={qualityPenalty}
                      onChange={(e) => setQualityPenalty(parseInt(e.target.value) || 0)}
                    />
                  </div>
                </div>
              </div>
              <DialogFooter>
                <Button variant="outline" onClick={() => setDialogOpen(false)}>
                  Cancel
                </Button>
                <Button onClick={handleCreateContract} disabled={!buyerCompanyId || !title || createContract.isPending}>
                  {createContract.isPending ? (
                    <>
                      <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                      Creating...
                    </>
                  ) : (
                    "Create Draft"
                  )}
                </Button>
              </DialogFooter>
            </DialogContent>
          </Dialog>
        </div>

        {/* Stats Cards */}
        <div className="grid gap-4 md:grid-cols-4">
          <Card>
            <CardContent className="pt-6">
              <div className="flex items-center gap-4">
                <div className="p-3 rounded-full bg-green-500/20">
                  <CheckCircle2 className="w-6 h-6 text-green-500" />
                </div>
                <div>
                  <p className="text-sm text-muted-foreground">Active Contracts</p>
                  <p className="text-2xl font-bold">{activeContracts}</p>
                </div>
              </div>
            </CardContent>
          </Card>
          <Card>
            <CardContent className="pt-6">
              <div className="flex items-center gap-4">
                <div className="p-3 rounded-full bg-blue-500/20">
                  <Clock className="w-6 h-6 text-blue-500" />
                </div>
                <div>
                  <p className="text-sm text-muted-foreground">Pending</p>
                  <p className="text-2xl font-bold">{pendingContracts}</p>
                </div>
              </div>
            </CardContent>
          </Card>
          <Card>
            <CardContent className="pt-6">
              <div className="flex items-center gap-4">
                <div className="p-3 rounded-full bg-purple-500/20">
                  <FileText className="w-6 h-6 text-purple-500" />
                </div>
                <div>
                  <p className="text-sm text-muted-foreground">Total Contracts</p>
                  <p className="text-2xl font-bold">{contracts?.length || 0}</p>
                </div>
              </div>
            </CardContent>
          </Card>
          <Card>
            <CardContent className="pt-6">
              <div className="flex items-center gap-4">
                <div className="p-3 rounded-full bg-yellow-500/20">
                  <DollarSign className="w-6 h-6 text-yellow-500" />
                </div>
                <div>
                  <p className="text-sm text-muted-foreground">Total Value</p>
                  <p className="text-2xl font-bold">${totalValue.toLocaleString()}</p>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>

        <div className="grid gap-6 lg:grid-cols-3">
          {/* Contract List */}
          <Card className="lg:col-span-1">
            <CardHeader>
              <div className="flex items-center justify-between">
                <CardTitle>Contracts</CardTitle>
                <Select value={statusFilter} onValueChange={setStatusFilter}>
                  <SelectTrigger className="w-32">
                    <SelectValue placeholder="Filter" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="all">All</SelectItem>
                    <SelectItem value="draft">Draft</SelectItem>
                    <SelectItem value="proposed">Proposed</SelectItem>
                    <SelectItem value="active">Active</SelectItem>
                    <SelectItem value="completed">Completed</SelectItem>
                    <SelectItem value="cancelled">Cancelled</SelectItem>
                  </SelectContent>
                </Select>
              </div>
            </CardHeader>
            <CardContent className="space-y-2">
              {contracts && contracts.length > 0 ? (
                contracts.map((contract) => (
                  <button
                    key={contract.id}
                    onClick={() => setSelectedContract(contract.id)}
                    className={`w-full p-3 rounded-lg border text-left transition-colors ${
                      selectedContract === contract.id
                        ? "border-primary bg-primary/10"
                        : "border-border hover:border-primary/50"
                    }`}
                  >
                    <div className="flex items-start justify-between mb-2">
                      <p className="font-medium truncate">{contract.title}</p>
                      {getStatusBadge(contract.status)}
                    </div>
                    <div className="flex items-center gap-2 text-sm text-muted-foreground">
                      <Building2 className="w-4 h-4" />
                      <span>
                        {contract.sellerCompanyId === myCompany?.id ? (
                          <>Buyer: {getCompanyName(contract.buyerCompanyId)}</>
                        ) : (
                          <>Seller: {getCompanyName(contract.sellerCompanyId)}</>
                        )}
                      </span>
                    </div>
                    <div className="flex items-center gap-2 text-sm text-muted-foreground mt-1">
                      <DollarSign className="w-4 h-4" />
                      <span>${parseFloat(contract.totalValue).toLocaleString()}</span>
                    </div>
                  </button>
                ))
              ) : (
                <div className="text-center py-8">
                  <FileText className="w-10 h-10 text-muted-foreground mx-auto mb-2" />
                  <p className="text-muted-foreground">No contracts found</p>
                </div>
              )}
            </CardContent>
          </Card>

          {/* Contract Details */}
          <Card className="lg:col-span-2">
            {contractDetails ? (
              <>
                <CardHeader>
                  <div className="flex items-start justify-between">
                    <div>
                      <CardTitle>{contractDetails.contract.title}</CardTitle>
                      <CardDescription>
                        {contractDetails.contract.description || "No description provided"}
                      </CardDescription>
                    </div>
                    {getStatusBadge(contractDetails.contract.status)}
                  </div>
                </CardHeader>
                <CardContent className="space-y-6">
                  {/* Contract Info */}
                  <div className="grid grid-cols-2 gap-4">
                    <div className="p-4 rounded-lg bg-muted">
                      <p className="text-sm text-muted-foreground mb-1">Seller</p>
                      <p className="font-medium flex items-center gap-2">
                        <Building2 className="w-4 h-4" />
                        {getCompanyName(contractDetails.contract.sellerCompanyId)}
                        {contractDetails.contract.sellerCompanyId === myCompany?.id && (
                          <Badge variant="outline">You</Badge>
                        )}
                      </p>
                    </div>
                    <div className="p-4 rounded-lg bg-muted">
                      <p className="text-sm text-muted-foreground mb-1">Buyer</p>
                      <p className="font-medium flex items-center gap-2">
                        <Building2 className="w-4 h-4" />
                        {getCompanyName(contractDetails.contract.buyerCompanyId)}
                        {contractDetails.contract.buyerCompanyId === myCompany?.id && (
                          <Badge variant="outline">You</Badge>
                        )}
                      </p>
                    </div>
                  </div>

                  <div className="grid grid-cols-3 gap-4 text-sm">
                    <div>
                      <p className="text-muted-foreground">Duration</p>
                      <p className="font-medium flex items-center gap-1">
                        <Calendar className="w-4 h-4" />
                        Turn {contractDetails.contract.startTurn} - {contractDetails.contract.endTurn}
                      </p>
                    </div>
                    <div>
                      <p className="text-muted-foreground">Delivery</p>
                      <p className="font-medium flex items-center gap-1">
                        <Clock className="w-4 h-4" />
                        {getDeliveryFrequencyLabel(contractDetails.contract.deliveryFrequency)}
                      </p>
                    </div>
                    <div>
                      <p className="text-muted-foreground">Penalties</p>
                      <p className="font-medium flex items-center gap-1">
                        <Percent className="w-4 h-4" />
                        Late: {parseFloat(contractDetails.contract.latePenaltyPercent)}% / Quality: {parseFloat(contractDetails.contract.qualityPenaltyPercent)}%
                      </p>
                    </div>
                  </div>

                  {/* Contract Items */}
                  <div>
                    <div className="flex items-center justify-between mb-3">
                      <h4 className="font-semibold">Contract Items</h4>
                      {contractDetails.contract.status === "draft" && (
                        <Dialog open={itemDialogOpen} onOpenChange={setItemDialogOpen}>
                          <DialogTrigger asChild>
                            <Button size="sm" variant="outline">
                              <Plus className="w-4 h-4 mr-2" />
                              Add Item
                            </Button>
                          </DialogTrigger>
                          <DialogContent>
                            <DialogHeader>
                              <DialogTitle>Add Contract Item</DialogTitle>
                              <DialogDescription>
                                Specify the goods to be delivered under this contract
                              </DialogDescription>
                            </DialogHeader>
                            <div className="grid gap-4 py-4">
                              <div className="space-y-2">
                                <Label>Resource</Label>
                                <Select value={resourceTypeId} onValueChange={setResourceTypeId}>
                                  <SelectTrigger>
                                    <SelectValue placeholder="Select resource" />
                                  </SelectTrigger>
                                  <SelectContent>
                                    {resources?.map((resource) => (
                                      <SelectItem key={resource.id} value={resource.id.toString()}>
                                        {resource.name}
                                      </SelectItem>
                                    ))}
                                  </SelectContent>
                                </Select>
                              </div>
                              <div className="grid grid-cols-2 gap-4">
                                <div className="space-y-2">
                                  <Label>Quantity per Delivery</Label>
                                  <Input
                                    type="number"
                                    min={1}
                                    value={quantityPerDelivery}
                                    onChange={(e) => setQuantityPerDelivery(parseInt(e.target.value) || 1)}
                                  />
                                </div>
                                <div className="space-y-2">
                                  <Label>Total Quantity</Label>
                                  <Input
                                    type="number"
                                    min={quantityPerDelivery}
                                    value={totalQuantity}
                                    onChange={(e) => setTotalQuantity(parseInt(e.target.value) || 1)}
                                  />
                                </div>
                              </div>
                              <div className="grid grid-cols-2 gap-4">
                                <div className="space-y-2">
                                  <Label>Price per Unit ($)</Label>
                                  <Input
                                    type="number"
                                    min={0.01}
                                    step={0.01}
                                    value={pricePerUnit}
                                    onChange={(e) => setPricePerUnit(parseFloat(e.target.value) || 0)}
                                  />
                                </div>
                                <div className="space-y-2">
                                  <Label>Minimum Quality (0-1)</Label>
                                  <Input
                                    type="number"
                                    min={0}
                                    max={1}
                                    step={0.05}
                                    value={minQuality}
                                    onChange={(e) => setMinQuality(parseFloat(e.target.value) || 0)}
                                  />
                                </div>
                              </div>
                              <div className="p-3 rounded-lg bg-muted text-sm">
                                <p className="text-muted-foreground">Item Value:</p>
                                <p className="font-medium text-lg">
                                  ${(totalQuantity * pricePerUnit).toLocaleString()}
                                </p>
                              </div>
                            </div>
                            <DialogFooter>
                              <Button variant="outline" onClick={() => setItemDialogOpen(false)}>
                                Cancel
                              </Button>
                              <Button onClick={handleAddItem} disabled={!resourceTypeId || addItem.isPending}>
                                {addItem.isPending ? (
                                  <>
                                    <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                                    Adding...
                                  </>
                                ) : (
                                  "Add Item"
                                )}
                              </Button>
                            </DialogFooter>
                          </DialogContent>
                        </Dialog>
                      )}
                    </div>

                    {contractDetails.items.length > 0 ? (
                      <div className="space-y-3">
                        {contractDetails.items.map((item) => (
                          <div key={item.id} className="p-4 rounded-lg border bg-card">
                            <div className="flex items-start justify-between mb-2">
                              <div className="flex items-center gap-2">
                                <Package className="w-5 h-5 text-muted-foreground" />
                                <span className="font-medium">{getResourceName(item.resourceTypeId)}</span>
                              </div>
                              <Badge variant="outline">
                                Quality min: {(parseFloat(item.minQuality) * 100).toFixed(0)}%
                              </Badge>
                            </div>
                            <div className="grid grid-cols-3 gap-4 text-sm">
                              <div>
                                <p className="text-muted-foreground">Per Delivery</p>
                                <p className="font-medium">{parseFloat(item.quantityPerDelivery).toFixed(0)} units</p>
                              </div>
                              <div>
                                <p className="text-muted-foreground">Total</p>
                                <p className="font-medium">{parseFloat(item.totalQuantity).toFixed(0)} units</p>
                              </div>
                              <div>
                                <p className="text-muted-foreground">Price</p>
                                <p className="font-medium">${parseFloat(item.pricePerUnit).toFixed(2)}/unit</p>
                              </div>
                            </div>
                            <div className="mt-2 text-sm">
                              <p className="text-muted-foreground">
                                Delivered: {parseFloat(item.deliveredQuantity).toFixed(0)} / {parseFloat(item.totalQuantity).toFixed(0)}
                              </p>
                              <div className="w-full bg-muted rounded-full h-2 mt-1">
                                <div
                                  className="bg-green-500 h-2 rounded-full"
                                  style={{
                                    width: `${(parseFloat(item.deliveredQuantity) / parseFloat(item.totalQuantity)) * 100}%`
                                  }}
                                />
                              </div>
                            </div>
                          </div>
                        ))}
                      </div>
                    ) : (
                      <div className="text-center py-8 border rounded-lg border-dashed">
                        <Package className="w-10 h-10 text-muted-foreground mx-auto mb-2" />
                        <p className="text-muted-foreground">No items in this contract</p>
                        {contractDetails.contract.status === "draft" && (
                          <p className="text-sm text-muted-foreground mt-1">
                            Add items to define what will be delivered
                          </p>
                        )}
                      </div>
                    )}
                  </div>

                  {/* Actions */}
                  <div className="flex items-center justify-between pt-4 border-t">
                    <div className="text-lg font-semibold">
                      Total Value: ${parseFloat(contractDetails.contract.totalValue).toLocaleString()}
                    </div>
                    <div className="flex gap-2">
                      {contractDetails.contract.status === "draft" &&
                        contractDetails.contract.sellerCompanyId === myCompany?.id &&
                        contractDetails.items.length > 0 && (
                          <Button onClick={() => proposeContract.mutate({ contractId: contractDetails.contract.id })}>
                            <Send className="w-4 h-4 mr-2" />
                            Propose Contract
                          </Button>
                        )}
                      {contractDetails.contract.status === "proposed" &&
                        contractDetails.contract.proposedBy !== myCompany?.id && (
                          <>
                            <Button
                              variant="outline"
                              onClick={() => rejectContract.mutate({ contractId: contractDetails.contract.id })}
                            >
                              <XCircle className="w-4 h-4 mr-2" />
                              Reject
                            </Button>
                            <Button onClick={() => acceptContract.mutate({ contractId: contractDetails.contract.id })}>
                              <CheckCircle2 className="w-4 h-4 mr-2" />
                              Accept
                            </Button>
                          </>
                        )}
                    </div>
                  </div>
                </CardContent>
              </>
            ) : (
              <CardContent className="flex flex-col items-center justify-center py-12">
                <FileText className="w-12 h-12 text-muted-foreground mb-4" />
                <h3 className="text-lg font-semibold mb-2">Select a Contract</h3>
                <p className="text-muted-foreground text-center">
                  Choose a contract from the list to view details, or create a new one.
                </p>
              </CardContent>
            )}
          </Card>
        </div>
      </div>
    </GameLayout>
  );
}
